import {
    MapContainer,
    TileLayer,
    Marker,
    Popup,
    GeoJSON,
    CircleMarker,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet-defaulticon-compatibility/dist/leaflet-defaulticon-compatibility.css";
import "leaflet-defaulticon-compatibility";

import L from 'leaflet';

import pizzaImage from '../../public/images/noun-pizza-delivery-249955-FF001C.svg'
import { useEffect, useState } from "react";
import { DeliveryStatus } from "Models";
import axios from "axios";
import React from "react";

const Map = ({
    deliveryLocation,
    orderId
}: {
    deliveryLocation: [number, number];
    orderId: string;
}) => {

    const [deliveryStatus, setDeliveryStatus] = useState<DeliveryStatus>()

    const refreshValue = 1
    
    useEffect(() => {
        if (orderId !== undefined) {
            getDeliveryStatus(setDeliveryStatus)
          const interval = setInterval(() => getDeliveryStatus(setDeliveryStatus), refreshValue*1000)
          return () => clearInterval(interval)
    
        }
    
      }, [orderId, refreshValue])

    const getDeliveryStatus = async (fn: (ds: DeliveryStatus) => void) => {
    const res = await axios(`http://localhost:8082/orders/${orderId}`)
    fn(res.data.deliveryStatus)
    }

    const icon = new L.Icon({
        iconUrl: pizzaImage.src,
        iconRetinaUrl: pizzaImage.src,
        popupAnchor:  [-0, -0],
        iconSize: [48,67.5],    
    });

    const currentLoc = deliveryStatus !== undefined ? [deliveryStatus.deliveryLat, deliveryStatus.deliveryLon] : [12.978268132410502, 77.59408889388118]
    const timestamp = deliveryStatus !== undefined ? deliveryStatus.ts : ""

    return (
        <MapContainer id="map" center={[12.978268132410502, 77.59408889388118]} zoom={12} scrollWheelZoom={true} style={{ height: '50vh', width: '50wh' }}>
            <TileLayer id="tileLayer"
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png"
            />
            {deliveryLocation[0] !== undefined && <Marker position={deliveryLocation}>
                <Popup>
                    Delivery Location <br />
                    ({deliveryLocation[0]}, {deliveryLocation[1]}
                    </Popup>
            </Marker>}
             <Marker position={currentLoc} icon={icon}>
                <Popup>Current Location <br />({currentLoc[0]}, {currentLoc[1]})<br/> {timestamp}</Popup>
            </Marker>
        </MapContainer>

    );
};

export default Map;