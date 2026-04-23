import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { StompProvider } from "./live/StompContext";
import App from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <StompProvider>
          <App />
        </StompProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
