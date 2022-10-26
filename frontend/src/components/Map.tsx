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

const Map = ({
    deliveryLocation, currentLocation, timestamp
}: {
    deliveryLocation: [number, number];
    currentLocation: [number, number];
    timestamp: string;
}) => {

    const icon = new L.Icon({
        iconUrl: pizzaImage.src,
        iconRetinaUrl: pizzaImage.src,
        popupAnchor:  [-0, -0],
        iconSize: [48,67.5],    
    });

    const currentLoc = currentLocation[0] !== undefined ? currentLocation : [12.978268132410502, 77.59408889388118]

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