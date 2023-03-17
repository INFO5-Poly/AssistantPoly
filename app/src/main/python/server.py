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
from googlesearch import search


def getSiteContent(url):
    html = urlopen(url).read()
    soup = BeautifulSoup(html, features="html.parser")

    # kill all script and style elements
    for script in soup(["script", "style", "nav"]):
        script.extract()    # rip it out

    # get text
    text = soup.get_text()
    return text.substring(0, text.length.coerceAtMost(3000))

def google_search(query):
    results = []
    for url in search(query, num_results=3):
        results.append(url)
    return results


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
        
        self.searching = False
        self.searchQuery = ""

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
        click.echo("handle lock in")
        with self.lock:
            self.complete = False
            self.msg = ""
            self.count += 1
            for r in self.bot.ask_stream(self.request_data):
                self.msg += r
                click.echo("handle lock out cond")
                self.condition.wait(timeout=0.2)
                click.echo("handle lock in cond")

            self.complete = True
            self.count += 1
            click.echo("handle lock out")



    def send_message(self, request_data):
        click.echo("send lock in")
        with self.lock:
            self.request_data = request_data
            self.request_event.set()

        click.echo("send lock out")

        

    def get_response(self):
        if self.isSearching():
            {
                "message": "searching...",
                "complete": self.complete
            }
        click.echo("response lock in")
        with self.lock:
            click.echo(self.msg)
            click.echo(self.complete)
            return {
                    "message": self.msg,
                    "complete": self.complete
                    }
    
    def reset_conversation(self):
        print("reset lock in")
        with self.lock:
            if self.count > 2:
                self.bot.rollback(self.count - 2)
                self.count = 0
        print("reset lock out")
        

    def run(self) -> None:
        self._startup()
        while not self._stopped():
            if self.isSearching():
                self.search()
            else:
                self._handle()
        self._shutdown()
    
    def setSearching(self, query):
        with self.lock:
            self.searching = True
            self.searchQuery = query
    
    def search(self):
        links = google_search(self.searchQuery)
        responses = []
        prompt = """System: search results:
            -------
            <CONTENT>
            -------
            Please write a short summary of the search results that answers the user's query. 
            """
        for link in links:
            content = getSiteContent(link)
            
            r = self.bot.ask(prompt.replace("<CONTENT>", content))
            self.count += 2
            print("Intermediate result")
            print(r)
            responses.append(r)
            self.bot.rollback(2)
            self.count -= 2
        
        p = prompt.replace("<CONTENT>", "\n\n".join(responses))
        self.send_message(p)
            
        with self.lock:
            self.searching = False


    def isSearching(self):
        with self.lock:
            return self.searching

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

    click.echo("exit set key")
    return (jsonify({"msg": "OK"}), 200)

@app.post("/message")
def post_message():
    global gthread
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    if gthread.isSearching():
        return (jsonify({"msg": "searching"}), 500)
    
    r = request.get_json()
    gthread.send_message(r["message"])
    click.echo("exit post message")
    return (jsonify({"msg": "OK"}), 200)

@app.post("/reset")
def reset():
    global gthread
    
    gthread.reset_conversation()
    click.echo("exit get response")
    return (jsonify({"msg": "OK"}), 200)

@app.post("/search")
def search():
    global gthread
    if not request.is_json:
        return {"error": "Request must be JSON"}, 415
    
    r = request.get_json()
    gthread.setSearching(r["query"])
    
    return (jsonify({"msg": "OK"}), 200)

@app.get("/response")
def get_response():
    global gthread
    r = gthread.get_response()
    click.echo("response lock out")

    jr = jsonify(r)
    click.echo("exit get response")
    return jr, 200

@app.get("/")
def index():
    return jsonify({"message": "OK"}), 200

app.run(host="0.0.0.0", port=5000, debug=True)