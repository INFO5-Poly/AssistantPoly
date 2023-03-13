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
    def __init__(self):
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
        self.condition = threading.Condition(self.lock)        

    def stop(self) -> None:
        self._stop_event.set()

    def _stopped(self) -> bool:
        return self._stop_event.is_set()

    def _startup(self) -> None:
        with open('openai.key', 'r') as file:
            self.config = file.read()
        
        with open('prompt.txt', 'r') as file:
            self.prompt = file.read()
        
        self.bot = gpt.Chatbot(api_key=self.config)
        request(self.prompt)

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
            for r in self.bot.ask_stream(self.request_data, conversation_id = self.conv_id):
                self.msg += r
                self.condition.wait(timeout=0.2)

            self.complete = True
            self.count += 1
            print("handle ok")
        

    def request(self, request_data):
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

app = Flask(__name__)

gthread = GPT_Thread()
gthread.start()

@app.post("/message")
def post_message():
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    r = request.get_json()
    gthread.request(r["message"])
    return "OK"

@app.post("/reset")
def reset():
    gthread.reset_conversation()
    return "OK"

@app.get("/response")
def get_response():
    return jsonify({"response": gthread.get_response()})

app.run(debug=True)