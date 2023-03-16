# app.py
from flask import Flask, request, jsonify, Response
import threading
import time
from queue import Queue
import revChatGPT.V3 as gpt
import json
import random
import string
from urllib.request import urlopen
from bs4 import BeautifulSoup
import click

class GPT_Thread(threading.Thread):
    def __init__(self, key):
        super().__init__()
        self._stop_event = threading.Event()
        self.request_event = threading.Event()
        self.lock = threading.Lock()
        self.request_data = ""
        self.bot = None
        self.msg = ""
        self.complete = False
        self.prompt = ""
        self.count = 0
        self.key = key
        self.condition = threading.Condition(self.lock)        

    def stop(self) -> None:
        self._stop_event.set()

    def _stopped(self) -> bool:
        return self._stop_event.is_set()

    def _startup(self) -> None:
        with open('prompt.txt', 'r') as file:
            self.prompt = file.read()
        
        self.bot = gpt.Chatbot(api_key=self.key)
        self.send_message(self.prompt)

    def _shutdown(self) -> None:
        pass

    def _handle(self) -> None:
        self.request_event.wait()
        self.request_event.clear()
        with self.lock:
            self.complete = False
            self.msg = ""
            self.count += 1
            for r in self.bot.ask_stream(self.request_data):
                self.msg += r
                self.condition.wait(timeout=0.2)

            self.complete = True
            self.count += 1


    def send_message(self, request_data):
        with self.lock:
            self.request_data = request_data
            self.request_event.set()

    def get_response(self):

        with self.lock:
            click.echo(self.msg)
            click.echo(self.complete)
            return {
                    "message": self.msg,
                    "complete": self.complete
                    }
    
    def reset_conversation(self):
        with self.lock:
            if self.count > 2:
                self.bot.rollback(self.count - 2)
                self.count = 0

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
    gthread = GPT_Thread(key)
    gthread.start()

    return ('', 200)

@app.post("/message")
def post_message():
    global gthread
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415

    r = request.get_json()
    gthread.send_message(r["message"])

    return ('', 200)

@app.post("/reset")
def reset():
    global gthread
    gthread.reset_conversation()
    return ('', 200)

@app.get("/response")
def get_response():
    global gthread
    r = gthread.get_response()
    return jsonify(r), 200

@app.get("/")
def index():
    return jsonify({"message": "OK"}), 200

app.run(host="0.0.0.0", port=5000, debug=True)