const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const POST_ID = "vahf_post_yoga_wheel";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("posts")
      .doc(POST_ID)
      .collection("likes")
      .where("postId", "==", POST_ID)
      .get()
  );
  await assertFails(
    unauthDb
      .collection("posts")
      .doc(POST_ID)
      .collection("comments")
      .where("postId", "==", POST_ID)
      .get()
  );
  await assertFails(
    unauthDb.collection("classes").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("live_sessions").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sharma",
      email: "alice@example.com",
      registrationCompleted: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile: owner create, partial update, and strict PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror UserProfileRepository create payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sharma",
      email: "alice@example.com",
      mobileNumber: "9876543210",
      city: "Bengaluru",
      wellnessGoal: "Daily Pranayama",
      registrationCompleted: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner read succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Owner partial update succeeds while preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice K. Sharma",
      city: "Mysuru",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user PII read & write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      city: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / privilege escalation & immutable field tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Post likes: one-like-per-user key prevents duplicates and cross-user tampering fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const likesRef = aliceDb.collection("posts").doc(POST_ID).collection("likes");

  // Query on empty subcollection with matching postId filter succeeds
  await assertSucceeds(likesRef.where("postId", "==", POST_ID).get());

  // Unfiltered list query fails
  await assertFails(likesRef.get());

  // Alice likes the post using docId = ALICE_UID
  await assertSucceeds(
    likesRef.doc(ALICE_UID).set({
      userId: ALICE_UID,
      postId: POST_ID,
      createdAt: now,
    })
  );

  // Bob cannot impersonate Alice or create a like under another doc ID
  await assertFails(
    bobDb.collection("posts").doc(POST_ID).collection("likes").doc(ALICE_UID).set({
      userId: ALICE_UID,
      postId: POST_ID,
      createdAt: now,
    })
  );
  await assertFails(
    bobDb.collection("posts").doc(POST_ID).collection("likes").doc("random_dup_id").set({
      userId: BOB_UID,
      postId: POST_ID,
      createdAt: now,
    })
  );

  // Bob cannot delete Alice's like
  await assertFails(
    bobDb.collection("posts").doc(POST_ID).collection("likes").doc(ALICE_UID).delete()
  );

  // Bob can query likes for count and add his own like
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc(POST_ID)
      .collection("likes")
      .where("postId", "==", POST_ID)
      .get()
  );
  await assertSucceeds(
    bobDb.collection("posts").doc(POST_ID).collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      postId: POST_ID,
      createdAt: now,
    })
  );

  // Alice can unlike her own like
  await assertSucceeds(likesRef.doc(ALICE_UID).delete());
});

test("Post comments: visible to registered users, author-only edit/delete, strict bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const aliceComments = aliceDb.collection("posts").doc(POST_ID).collection("comments");
  const bobComments = bobDb.collection("posts").doc(POST_ID).collection("comments");

  // Empty query with postId filter succeeds; unfiltered fails
  await assertSucceeds(aliceComments.where("postId", "==", POST_ID).get());
  await assertFails(aliceComments.get());

  // Alice posts a comment
  await assertSucceeds(
    aliceComments.doc("comment_1").set({
      userId: ALICE_UID,
      postId: POST_ID,
      authorName: "Alice Sharma",
      text: "Wonderful balance session!",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob (another registered user) can read comments on the post
  await assertSucceeds(bobComments.where("postId", "==", POST_ID).get());

  // Bob cannot edit or delete Alice's comment
  await assertFails(
    bobComments.doc("comment_1").update({
      text: "Edited by Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobComments.doc("comment_1").delete());

  // Reject oversized or empty comment text and future timestamps
  await assertFails(
    bobComments.doc("comment_2").set({
      userId: BOB_UID,
      postId: POST_ID,
      authorName: "Bob",
      text: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobComments.doc("comment_3").set({
      userId: BOB_UID,
      postId: POST_ID,
      authorName: "Bob",
      text: "Valid text",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Alice can delete her own comment
  await assertSucceeds(aliceComments.doc("comment_1").delete());
});

test("Published classes, live sessions, and feed posts are read-only when published == true", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("classes").doc("cls_1").set({
      title: "Morning Hatha",
      scheduleText: "Mon 7:00 AM",
      category: "Hatha",
      published: true,
    });
    await db.collection("classes").doc("cls_draft").set({
      title: "Draft Class",
      scheduleText: "TBD",
      category: "Draft",
      published: false,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("classes").where("published", "==", true).get()
  );
  await assertFails(aliceDb.collection("classes").get());
  await assertFails(aliceDb.collection("classes").doc("cls_draft").get());
  await assertFails(
    aliceDb.collection("classes").doc("cls_2").set({
      title: "Fake Class",
      scheduleText: "Now",
      category: "Fake",
      published: true,
    })
  );
  await assertFails(
    aliceDb.collection("live_sessions").doc("live_1").set({
      title: "Fake Live",
      scheduleText: "Now",
      meetingUrl: "https://example.com",
      published: true,
    })
  );
  await assertSucceeds(
    aliceDb.collection("live_sessions").where("published", "==", true).get()
  );
  await assertSucceeds(
    aliceDb.collection("feed_posts").where("published", "==", true).get()
  );
});

test("Registrations: schema validation, owner isolation, query parity, and terms check", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);
  const regId = `${ALICE_UID}_abcd1234efgh5678`;

  const validPayload = {
    userId: ALICE_UID,
    submissionHash: "abcd1234efgh5678ijkl9012",
    profileImageBase64: "data_base64_sample_jpeg_payload_ok",
    profileImageName: "applicant_photo.jpg",
    fullName: "Alice Sharma",
    email: "alice@example.com",
    phoneNumber: "9876543210",
    phoneVerified: true,
    verifiedWhatsappNumber: "919876543210",
    occupation: "Software Engineer",
    address: "Marathahalli, Bengaluru",
    ageYears: "28",
    dateOfBirth: "15/06/1998",
    membersCount: "1",
    joiningMonths: 3,
    monthlyFeeInr: 1500,
    totalFeeInr: 4500,
    feeAutomationEnabled: true,
    nextDueDateMillis: Date.now() + 30 * 24 * 60 * 60 * 1000,
    lastNotifiedMonth: 0,
    selectedClasses: ["Online Yoga Class", "At Center Yoga Classes"],
    selectedPackages: ["Third month"],
    healthIssues: "None",
    expectations: "Flexibility and pranayama",
    gender: "Female",
    maritalStatus: "Single",
    height: "165 cm",
    weight: "58 kg",
    dateOfJoining: "25/11/2026",
    preferredTime: "07:00 AM",
    medicationDetails: "None",
    referralSource: "Website",
    termsAccepted: true,
    termsSignature: "I Agree",
    medicalReportNames: ["checkup.pdf"],
    medicalReportPreviews: [],
    notificationChannel: "whatsapp",
    notificationDispatched: false,
    createdAt: past,
    updatedAt: past,
  };

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("registrations").doc(regId).set(validPayload));
  await assertFails(
    unauthDb.collection("registrations").where("userId", "==", ALICE_UID).get()
  );

  // Invalid gender, unaccepted terms, unverified phone, or out-of-range joiningMonths rejected
  await assertFails(
    aliceDb.collection("registrations").doc(`${ALICE_UID}_bad1`).set({
      ...validPayload,
      termsAccepted: false,
    })
  );
  await assertFails(
    aliceDb.collection("registrations").doc(`${ALICE_UID}_bad2`).set({
      ...validPayload,
      gender: "Unknown",
    })
  );
  await assertFails(
    aliceDb.collection("registrations").doc(`${ALICE_UID}_bad3`).set({
      ...validPayload,
      phoneVerified: false,
    })
  );
  await assertFails(
    aliceDb.collection("registrations").doc(`${ALICE_UID}_bad4`).set({
      ...validPayload,
      joiningMonths: 0,
    })
  );

  // Wrong prefix registrationId rejected
  await assertFails(
    aliceDb.collection("registrations").doc("not_alice_prefix").set(validPayload)
  );

  // Owner duplicate-check get() on non-existent doc must succeed; cross-user get() must fail
  await assertSucceeds(aliceDb.collection("registrations").doc(regId).get());
  await assertFails(bobDb.collection("registrations").doc(regId).get());

  // Owner create succeeds
  await assertSucceeds(
    aliceDb.collection("registrations").doc(regId).set(validPayload)
  );

  // Owner list query with userId filter succeeds; unfiltered list fails
  await assertSucceeds(
    aliceDb.collection("registrations").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("registrations").get());

  // Cross-user read & update fail
  await assertFails(bobDb.collection("registrations").doc(regId).get());
  await assertFails(
    bobDb.collection("registrations").doc(regId).update({
      notificationDispatched: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner partial update of notification status and monthly fee automation progress succeeds; editing form answers fails
  await assertSucceeds(
    aliceDb.collection("registrations").doc(regId).update({
      notificationChannel: "webhook+whatsapp",
      notificationDispatched: true,
      lastNotifiedMonth: 1,
      nextDueDateMillis: Date.now() + 60 * 24 * 60 * 60 * 1000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("registrations").doc(regId).update({
      fullName: "Tampered Name",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("registrations").doc(regId).update({
      monthlyFeeInr: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

