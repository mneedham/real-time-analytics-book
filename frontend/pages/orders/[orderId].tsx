import * as React from 'react';
import { useEffect, useState } from 'react';
import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

import Button from '@mui/material/Button';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { Order, OrderStatus, OrderSummary } from 'Models';

import { useRouter } from 'next/router'

import axios from 'axios';

import { ButtonAppBar } from '../../src/ButtonAppBar';
import Link from 'next/link';
const mdTheme = createTheme();

import dynamic from "next/dynamic";

const DEFAULT_CENTER = [38.907132, -77.036546]  

export default function Home() {
  const router = useRouter()
  const { orderId } = router.query

  const [order, setOrder] = useState<Order>()

  useEffect(() => {
    if (orderId !== undefined) {
      getOrder(setOrder)
    }

  }, [orderId])

  const MapWithNoSSR = dynamic(() => import("../../src/components/Map"), {
    ssr: false,
  });


  const getOrder = async (fn: (orders: Order) => void) => {
    const res = await axios(`http://localhost:5000/orders/${orderId}`)
    fn(res.data)
  }

  return (
    <ThemeProvider theme={mdTheme}>
      <ButtonAppBar />
      <Box
        sx={{
          my: 4,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <Container>

          {order && <Link href={`/users/${order.userId}`}>

            <Button
              className="ml-0 pl-0"
              onClick={() => {
                getOrder(setOrder)
              }}>
              🔙 to user {order.userId}
            </Button>

          </Link>}

          <Typography variant="h4" component="h1" >
            Order {orderId}
          </Typography>


          <div className="mt-2">
            <div className="flex justify-between">
              <Typography variant="h5" component="h1">Status</Typography>
              <Button
                className="ml-0 pl-0"
                onClick={() => {
                  getOrder(setOrder)
                }}>
                Refresh Status
              </Button>

            </div>


            {order && order.statuses.map(row => (
              <div className={"px-2 py-5 border-2 border-indigo-200 my-5 rounded-lg flex" + (row.status === "DELIVERED" ? " bg-green-400" : "")}>
                <div className="w-48">{row.timestamp}</div>
                <div className="font-semibold">{row.status}</div>
              </div>
            ))}

            {(!order || order.statuses.length == 0) && <div>
              No statuses found for order
            </div>}

          </div>

          <Typography variant="h5" component="h1">Map</Typography>
          <div>
            <MapWithNoSSR 
              deliveryLocation={[order?.deliveryLat, order?.deliveryLon]}
              currentLocation={[order?.deliveryStatus?.deliveryLat, order?.deliveryStatus?.deliveryLon]}
            >

            </MapWithNoSSR>

          </div>

          <Typography variant="h5" component="h1">Items</Typography>
          <div className="px-2 py-4 border-2 border-sky-600 my-5 rounded-lg flex">

            <ul>
              {order?.products.map(product => (
                <li>
                  <div className="flex py-2 h-20">
                    <div className="items-center">
                      <img src={product.image} width="75px" />
                    </div>
                    <div className="flex ml-2 items-center">
                      <div className="w-64 align-baseline">{product.product}</div>
                      <div>{product.quantity} x {product.price}</div>

                    </div>

                  </div>
                </li>
              ))}
            </ul>
          </div>

        </Container>
      </Box>
    </ThemeProvider>
  );
}
