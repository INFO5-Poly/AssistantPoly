# app.py
from flask import Flask, request, jsonify
import threading
import time
from queue import Queue
import revChatGPT.V3 as gpt
import json
import random
import string
from urllib.request import urlopen
from bs4 import BeautifulSoup


class GPT_Thread(threading.Thread):
    def __init__(self, config):
        super().__init__()
        self._stop_event = threading.Event()
        self.request_event = threading.Event()
        self.lock = threading.Lock()
        self.request_data = ""
        self.bot = None
        self.msg = ""
        self.complete = True
        self.prompt = ""
        self.count = 0
        self.config = config
        self.condition = threading.Condition(self.lock)        

    def stop(self) -> None:
        self._stop_event.set()

    def _stopped(self) -> bool:
        return self._stop_event.is_set()

    def _startup(self) -> None:
        with open('prompt.txt', 'r') as file:
            self.prompt = file.read()
        
        self.bot = gpt.Chatbot(api_key=self.config)
        self.send_message(self.prompt)

    def _shutdown(self) -> None:

        pass

    def _handle(self) -> None:
        with self.lock:
            print("handle")
            self.request_event.wait()
            print("handle wait over")
            self.request_event.clear()
            
            self.complete = False

            self.count += 1
            for r in self.bot.ask_stream(self.request_data):
                self.msg += r
                self.condition.wait(timeout=0.2)

            self.complete = True
            self.count += 1
            print("handle ok")
        

    def send_message(self, request_data):
        with self.lock:
            print("request: " + request_data)
            self.request_data = request_data
            self.request_event.set()
            print("request ok")
        
    def get_response(self):
        with self.lock:
            print("get response")
            return {
                    "message": self.msg,
                    "complete": self.complete
                }
    
    def reset_conversation(self):
        with self.lock:
            if(self.count > 2):
                self.bot.rollback(self.count - 2)

    def run(self) -> None:
        self._startup()
        while not self._stopped():
            self._handle()
        self._shutdown()

    def getSiteContent(url):
        html = urlopen(url).read()
        soup = BeautifulSoup(html, features="html.parser")

        # kill all script and style elements
        for script in soup(["script", "style", "nav"]):
            script.extract()    # rip it out

        # get text
        text = soup.get_text()
        return text.substring(0, text.length.coerceAtMost(3000))

gthread = None
app = Flask(__name__)



@app.post("/key")
def set_key():
    global gthread
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    r = request.get_json()
    key = r["key"]
    if(gthread != None):
        gthread.stop()
    gthread = GPT_Thread('{"key": "${key}"}')
    gthread.run()
    return "OK"

@app.post("/message")
def post_message():
    global gthread
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    r = request.get_json()
    gthread.send_message(r["message"])
    return "OK"

@app.post("/reset")
def reset():
    global gthread
    gthread.reset_conversation()
    return "OK"

@app.get("/response")
def get_response():
    global gthread
    return jsonify({"response": gthread.get_response()})

@app.get("/")
def index():
    return "OK"

app.run(host="0.0.0.0", port=5000, debug=True)