const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

exports.addMessage = functions.https.onRequest(async (req, res) => {
    // Grab the text parameter.
    const original = req.query.text;
    // Push the new message into the Realtime Database using the Firebase Admin SDK.
    const snapshot = await admin.database().ref('/messages').push({original: original});
    // Redirect with 303 SEE OTHER to the URL of the pushed object in the Firebase console.
    res.redirect(303, snapshot.ref.toString());
  });

exports.makeUppercase = functions.database.ref('/messages/{pushId}/original')
  .onCreate((snapshot, context) => {
    // Grab the current value of what was written to the Realtime Database.
    const original = snapshot.val();
    console.log('Uppercasing', context.params.pushId, original);
    const uppercase = original.toUpperCase();
    // You must return a Promise when performing asynchronous tasks inside a Functions such as
    // writing to the Firebase Realtime Database.
    // Setting an "uppercase" sibling in the Realtime Database returns a Promise.
    return snapshot.ref.parent.child('content').set(uppercase);
  });

exports.sendChatNotification = functions.database.ref('/messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    const newMessage = snapshot.val();
    if (newMessage.content == undefined) {
      return;
    }
    const tokensSnapshot = await admin.database().ref(`/users`).once('value');
    if (!tokensSnapshot.hasChildren()) {
        return console.log('There are no one subscribed to send notification to');
    }
    console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');

    const payload = {
        notification: {
            title: newMessage.name,
            body: newMessage.content
        }
    }

    var usersObject = tokensSnapshot.val();
    if (context.authType == 'USER') {
      delete usersObject[context.auth.uid];
    }
    tokens = Object.values(usersObject);
    
    const response = await admin.messaging().sendToDevice(tokens, payload);
    // For each message check if there was an error.
    const tokensToRemove = [];
    response.results.forEach((result, index) => {
      const error = result.error;
      if (error) {
        console.error('Failure sending notification to', tokens[index], error);
        // Cleanup the tokens who are not registered anymore.
        // if (error.code === 'messaging/invalid-registration-token' ||
        //     error.code === 'messaging/registration-token-not-registered') {
        //   tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
        // }
      }
    });
  });

exports.sendChatImageNotification = functions.database.ref('/messages/{messageId}')
  .onUpdate(async (snapshot, context) => {
    if (!snapshot.before.val().imgUrl.startsWith("http")) {
      console.log("cancelled");
      return;
    }
    const tokensSnapshot = await admin.database().ref(`/users`).once('value');
    if (!tokensSnapshot.hasChildren()) {
        return console.log('There are no one subscribed to send notification to');
    }
    console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');
    var newMessage = snapshot.after.val();
    const payload = {
      notification: {
        title: newMessage.name,
        image: newMessage.imgUrl
      }
    }
    
    var usersObject = tokensSnapshot.val();
    if (context.authType == 'USER') {
      delete usersObject[context.auth.uid];
    }
    tokens = Object.values(usersObject);
    
    const response = await admin.messaging().sendToDevice(tokens, payload);
    // For each message check if there was an error.
    const tokensToRemove = [];
    response.results.forEach((result, index) => {
      const error = result.error;
      if (error) {
        console.error('Failure sending notification to', tokens[index], error);
        // Cleanup the tokens who are not registered anymore.
        // if (error.code === 'messaging/invalid-registration-token' ||
        //     error.code === 'messaging/registration-token-not-registered') {
        //   tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
        // }
      }
    });
  });