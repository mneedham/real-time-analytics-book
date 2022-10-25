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

import icon2 from '../../public/images/noun-pizza-delivery-4649332-FF001C.svg'

const Map = ({
    deliveryLocation, currentLocation, timestamp
}: {
    deliveryLocation: [number, number];
    currentLocation: [number, number];
    timestamp: string;
}) => {

    const myCustomColour = 'red'

    console.log("deliveryLocation", deliveryLocation, "deliveryLocation[0] !== undefined", deliveryLocation[0] !== undefined)

    const markerHtmlStyles = `
      background-color: ${myCustomColour};
      width: 2rem;
      height: 2rem;
      display: block;
      left: -1.5rem;
      top: -1.5rem;
      position: relative;
      border-radius: 2rem 2rem 0;
      transform: rotate(45deg);
      border: 1px solid #FFFFFF`

    // const icon = L.divIcon({
    //     className: "my-custom-pin",
    //     iconAnchor: [0, 24],
    //     popupAnchor: [0, -36],
    //     html: `<span style="${markerHtmlStyles}" />`
    // })

    console.log("icon2", icon2)

    const icon = new L.Icon({
        iconUrl: icon2.src,
        iconRetinaUrl: icon2.src,
        popupAnchor:  [-0, -0],
        iconSize: [48,67.5],    
    });

    return (
        <MapContainer center={[12.978268132410502, 77.59408889388118]} zoom={12} scrollWheelZoom={true} style={{ height: '50vh', width: '50wh' }}>
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png"
            />
            {deliveryLocation[0] !== undefined && <Marker position={deliveryLocation}>
                <Popup>
                    Delivery Location <br />
                    ({deliveryLocation[0]}, {deliveryLocation[1]}
                    </Popup>
            </Marker>}
            {currentLocation[0] !== undefined && <Marker
                position={currentLocation}
                icon={icon}>
                <Popup>Current Location <br />({currentLocation[0]}, {currentLocation[1]})<br/> {timestamp}</Popup>
            </Marker>}
        </MapContainer>

    );
};

export default Map;