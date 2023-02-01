var recording = false;
document.getElementById("speak-bubble").addEventListener("click", () => {
    recording = !recording;
    AndroidAPI.setRecording(recording);
})
