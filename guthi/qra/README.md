# Preset donation QR images

Place PNG files here (same folder as your Spring Boot working directory when you run from the `guthi` module):

| File     | Amount |
| -------- | ------ |
| `50.png` | ₹50    |
| `1.png`  | ₹1,000 |
| `2.png`  | ₹2,000 |
| `3.png`  | ₹3,000 |
| `4.png`  | ₹4,000 |
| `5.png`  | ₹5,000 |

They are served at **`http://localhost:9090/qra/<filename>`** (and via the Vite dev proxy at **`http://localhost:5173/qra/<filename>`**).
