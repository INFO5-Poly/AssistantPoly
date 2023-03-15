
var state = 0;

var user_theme;

var lastMessage = null;

document.getElementById("speak-bubble").addEventListener("click", () => {
    if(state == 0)
        AndroidAPI.setListening(true);
    else
        AndroidAPI.setListening(false);

<<<<<<< HEAD
=======
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
    //AndroidAPI.openApplication("youtube");
   AndroidAPI.openYoutubeVideo ("recette pour faire du pure");
>>>>>>> 02eebf2f70713c9ee1edc0a773d9f70182dabcdb
})

function addMessage(isUser) {
    var template;
    if (isUser)
        template = document.querySelector("template").content.querySelector(".user-message");
    else
        template = document.querySelector("template").content.querySelector(".assistant-message");
    lastMessage = template.cloneNode(true);
    if(isUser)
        lastMessage.querySelector("p").innerHTML = "🎙️";
    else
        lastMessage.querySelector("p").innerHTML = "...";
    const main = document.querySelector("main");
    main.appendChild(lastMessage);
    document.querySelector("main").scrollTo(0, document.querySelector("main").scrollHeight);
}

function editMessage(msg){
    if(msg != "")
        lastMessage.querySelector("p").innerHTML = msg;
}

function deleteMessage(){
    lastMessage.remove();
}
function clear() {
    document.querySelector("main").innerHTML = ""
    lastMessage = null
}

function setState(s){
    state = s;

    if(s == 2){
        stateWaiting()
    }
    else if (s == 1) {
        stateListening();
    }
    else {
        stateIdle();
    }
}
function stateListening() {
    const speakingBar = document.getElementById("speaking-bar");
    speakingBar.style.width = "100%";
    document.getElementsByTagName("footer")[0].style.height = "30vh";
    document.getElementById("speak-icon").innerHTML = "close";
    document.getElementById("speak-icon").style.pointerEvents = "auto";
}

function stateIdle() {
    const speakingBar = document.getElementById("speaking-bar");
    speakingBar.style.width = "0%";
    document.getElementsByTagName("footer")[0].style.height = "8vh";
    document.getElementById("speak-icon").innerHTML = "mic";
    document.getElementById("speak-icon").style.pointerEvents = "auto";
}

function stateWaiting() {
    const speakingBar = document.getElementById("speaking-bar");
    speakingBar.style.width = "0%";
    document.getElementsByTagName("footer")[0].style.height = "8vh";
    document.getElementById("speak-icon").innerHTML = "pending";
    document.getElementById("speak-icon").style.pointerEvents = "none";
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
    var ApiKey = $(".input-group input")[0].value;
    AndroidAPI.apiKeyChanged(ApiKey);
})

AndroidAPI.ready();



