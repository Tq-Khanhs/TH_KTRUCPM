import express from "express"
import { createProxyMiddleware } from "http-proxy-middleware"
import cors from "cors"

const app = express()
const port = process.env.PORT || 3000

// Service URLs
const PRODUCT_SERVICE_URL = process.env.PRODUCT_SERVICE_URL || "http://product-service:3001"
const ORDER_SERVICE_URL = process.env.ORDER_SERVICE_URL || "http://order-service:3002"
const CUSTOMER_SERVICE_URL = process.env.CUSTOMER_SERVICE_URL || "http://customer-service:3003"

// Middleware
app.use(cors())
app.use(express.json())

// Health check
app.get("/health", (req, res) => {
  res.status(200).send("API Gateway is healthy")
})

// Routes
app.use(
  "/products",
  createProxyMiddleware({
    target: PRODUCT_SERVICE_URL,
    changeOrigin: true,
    pathRewrite: {
      "^/products": "/products",
    },
  }),
)

app.use(
  "/orders",
  createProxyMiddleware({
    target: ORDER_SERVICE_URL,
    changeOrigin: true,
    pathRewrite: {
      "^/orders": "/orders",
    },
  }),
)

app.use(
  "/customers",
  createProxyMiddleware({
    target: CUSTOMER_SERVICE_URL,
    changeOrigin: true,
    pathRewrite: {
      "^/customers": "/customers",
    },
  }),
)

// Start server
app.listen(port, () => {
  console.log(`API Gateway running on port ${port}`)
})
