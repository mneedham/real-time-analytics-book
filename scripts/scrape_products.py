from calendar import c
import requests
from bs4 import BeautifulSoup
import glob
import json

for file_name in glob.glob("raw_data/*"):
    # print(file_name)

    with open(file_name, "r") as file:
        soup = BeautifulSoup(file.read(), 'html.parser')

        name_element = soup.select("h1 span")

        if len(name_element) > 0:
            name = soup.select("h1 span")[0].text

            description_element = soup.select("section#main-section-mobile div.p-text-description.lead p")
            description = description_element[0].text if len(description_element) > 0  else ""
            
            category = [item.text for item in soup.select("ul.breadcrumb li a span")][-2]
            image = soup.select("div.col-md-6 img.element-left-40")[0]["src"]
            crusts = [item.text.strip() for item in soup.select("ul.ul-list.features-list.element-top-1 li")]

            crust_sizes_element = soup.select("div.col-md-12.col-xs-12 p.sub-description.lead")
            crust_sizes = [item.strip() for item in crust_sizes_element[0].text.strip().split("|")] if len(crust_sizes_element) > 0  else ["regular", "medium", "large"]

            prices_table = [[col.text.strip() for col in item.select("th")] for item in soup.select("section#main-section table tr")]
            prices = {item[0]: item[1] for item in prices_table}

            price = soup.select("section#main-section span.price.text-left")[0].text.strip()

            if price == "":
                continue

            item = {
                "name": " ".join([word.capitalize() for word in name.split(" ")]),
                "description": description,
                "price": price,
                "category": category,
                "image": image
            }

            if category != "beverages":
                item = {**item, 
                    "crusts": crusts,
                    "crustSizes": crust_sizes,
                    "prices": prices
                }

            print(json.dumps(item))