
var recording = false;
const feelings = {
    satisfied: "sentiment_satisfied",
    dissatisfied: "sentiment_dissatisfied",
    neutral: "sentiment_neutral"
}
var user_feeling = feelings.neutral;

//variable pour tester l'avatar
var feeling_count = 1;
var user_theme;


document.getElementById("speak-bubble").addEventListener("click", () => {

    //AndroidAPI.setRecording(recording);
   /* if (recording) {
        stopRecording();
    }
    else {
        startRecording();
    }
    addMessage(true, "Hello??????????????????????????????????????????????????????");
    addMessage(false, "Hello!!!");
    feeling_count = feeling_count % 3;
    switch (feeling_count) {
        case 1: changeUserFeeling("happy"); break;
        case 2: changeUserFeeling("sad"); break;
        case 0: changeUserFeeling("neutral"); break;
    }
    feeling_count++;*/
   // AndroidAPI.phoneCall("tel:0767642068");
    AndroidAPI.openApplication("taptap");
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

    //AdujstSpeakingBar();


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


//feeling : l'entree que chatGPT va nous donner 
function changeUserFeeling(feeling) {
    //adapter le switch selon le type de l'entree que chatGPT va nous rendre

    switch (feeling) {
        case "happy": user_feeling = feelings.satisfied; break;
        case "sad": user_feeling = feelings.dissatisfied; break;
        case "neutral": user_feeling = feelings.neutral; break;
    }

    document.getElementById("feeling_icon").innerHTML = user_feeling;


}
$(".dropdown-menu li a").click(function () {
    var selText = $(this).text();
    $(this).parents('.dropdown').find('.dropdown-toggle').html(selText + ' <span class="theme"></span>');
    user_theme = selText;
});


document.getElementById("save-button").addEventListener("click", () => {

    // document.body.style.backgroundColor=user_theme;
    switch (user_theme) {
        case "Dark mode":
            if (!document.body.classList.contains("dark-mode")) {
                document.body.classList.toggle("dark-mode");
                var elements =document.querySelectorAll(".chat-bubble-text");
                for (var i = 0; i < elements.length; i++) {
                    var element = elements[i];
                    element.classList.add("dark-mode");
                }
                var elements = document.querySelector("template").content.querySelectorAll(".chat-bubble-text");
                for (var i = 0; i < elements.length; i++) {
                    var element = elements[i];
                    element.classList.add("dark-mode");
                }
            }
            break;
        case "Light mode":
            if (document.body.classList.contains("dark-mode")) {
                document.body.classList.toggle("dark-mode");
                var elements =document.querySelectorAll(".chat-bubble-text");
                for (var i = 0; i < elements.length; i++) {
                    var element = elements[i];
                    element.classList.remove("dark-mode");
                }
                var elements = document.querySelector("template").content.querySelectorAll(".chat-bubble-text");
                for (var i = 0; i < elements.length; i++) {
                    var element = elements[i];
                    element.classList.remove("dark-mode");
                }
            }
            break;
    }








})



