# Real-Time Analytics Book Code Repository

The code for Dunith Dhanushka and Mark Needham's upcoming book on Real-Time Analytics.

*Put the architecture diagram here*

## Install components

```bash
docker-compose \
  -f docker-compose-base.yml \
  -f docker-compose-pinot.yml \
  -f docker-compose-dashboard-enriched.yml \
  -f docker-compose-dashboard.yml up
```

```bash
docker-compose \
  -f docker-compose-base.yml \
  -f docker-compose-pinot-m1.yml \
  -f docker-compose-dashboard.yml \
  -f docker-compose-dashboard-enriched.yml \
  up
```

Once that's run, you can navigate to the following:

* Pinot UI - http://localhost:9000
* Streamlit Dashboard - http://localhost:8501


## Add enriched table

```
docker run -v $PWD/pinot/config:/config \
  --network rta \
  apachepinot/pinot:0.11.0 \
  AddTable -schemaFile /config/orders_enriched/schema.json \
           -tableConfigFile /config/orders_enriched/table.json \
           -controllerHost pinot-controller -exec
```

## (Optional) Downloading products

Setup Python environment:

```bash
python -m venv venv
source venv/bin/activate
```

Install dependencies:

```bash
pip install -r scripts/requirements.txt
```

Downloading product pages:

```bash
python scripts/download_products.py
```

Scrape product data:

```bash
python scripts/scrape_products.py
```

The products will be written to [simulator/data/products.json](simulator/data/products.json)