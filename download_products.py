from calendar import c
import requests
import shutil
from bs4 import BeautifulSoup

import os

response = requests.get("https://www.dominos.co.in/product-item-pages.xml")

soup = BeautifulSoup(response.text, 'xml')

urls = [item.text for item in soup.select("url loc")]

for url in urls:
    print(url)
    file_name = f"raw_data/{url.split('/')[-1]}"
    
    if not os.path.isfile(file_name):    
        product_response = requests.get(url)
        with requests.get(url, stream=True) as r:
            with open(file_name, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192): 
                    f.write(chunk)