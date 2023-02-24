import revChatGPT.Official as gpt

key = ""
with open('openai.key', 'r') as file:
    key = file.read()


bot = gpt.Chatbot(key)

stream = bot.ask_stream("What languages do you speak?")

def get_response():
    a = next(stream)
    print(a)
    return a