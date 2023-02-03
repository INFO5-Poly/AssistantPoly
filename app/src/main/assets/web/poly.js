var recording = false;
 
document.getElementById("speak-bubble").addEventListener("click", () => {

    //AndroidAPI.setRecording(recording);
    if (recording) {
        stopRecording();
    }
    else {
        startRecording();
    }
    addMessage(true, "Hello???");
    addMessage(false, "Hello!!!");
})

function addMessage(isUser, msg) {
    var template;
    if (isUser)
        template = document.querySelector("template").content.querySelector(".user-message");
    else
        template = document.querySelector("template").content.querySelector(".assistant-message");
    const clone = template.cloneNode(true);
    const p = clone.querySelectorAll("p");
    p[0].innerHTML = msg;
    const main = document.querySelector("main");
    main.appendChild(clone);
    document.querySelector("main").scrollTo(0, document.querySelector("main").scrollHeight);

}
function startRecording() {
    recording = true;
    const speakingBar = document.getElementById("speaking-bar");
    speakingBar.style.width = "100%";
    document.getElementsByTagName("footer")[0].style.height = "30vh";
    document.getElementById("speak-icon").innerHTML = "send";

    AdujstSpeakingBar();


}

function stopRecording() {
    recording = false;
    const speakingBar = document.getElementById("speaking-bar");
    speakingBar.style.width = "0%";
    document.getElementsByTagName("footer")[0].style.height = "8vh";
    document.getElementById("speak-icon").innerHTML = "mic";

}

function AdujstSpeakingBar() {
    const recognition = new webkitSpeechRecognition();
    recognition.continuous = true;
    recognition.start();

    // Set up the event listener to update the width of the bar based on the audio levels
    recognition.addEventListener("result", (event) => {
        const result = event.results[event.resultIndex];
        const transcript = result[0].transcript;
        console.log(transcript);
        const confidence = result[0].confidence;
        const speakingBar = document.getElementById("speaking-bar");
        speakingBar.style.width = `${confidence * 100}%`;
    })
}




