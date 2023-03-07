import requests
import time

url = 'http://localhost:5000/message'
data = {'message': 'What languages do you speak?'}

response = requests.post(url, json=data)

print(response.status_code)
print(response.text)

url = 'http://localhost:5000/response'

for i in range(20):
    response = requests.get(url)

    print(response.status_code)
    print(response.text)
    time.sleep(0.5)