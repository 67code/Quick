import { Routes, Route } from "react-router-dom";
import Header from "./components/layouts/Header";
import Footer from "./components/layouts/Footer";
import HomePage from "./pages/HomePage";
import AuthForm from "./components/auth/AuthForm";
import Favorites from "./pages/Favorites";
import UserProfile from "./pages/Profile";
import RegisterService from "./pages/RegisterService";
import PrivateRoute from "./components/auth/PrivateRoute";
import { ToastContainer } from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';
import { useState } from "react"
import ServicesPage from "./pages/ServicePage";
import ServiceDetailPage from "./pages/ServiceDetailPage";
import BusinessProfile from "./pages/BuisnessProfile";
import BookingManagement  from "./components/sections/BookingManagement"; 
function App() {
  const [selectedCity, setSelectedCity] = useState("Mumbai");

  return (
    <div className="flex flex-col min-h-screen">
      <Header selectedCity={selectedCity} setSelectedCity={setSelectedCity} />
      <Routes>
        <Route path="/" element={<HomePage selectedCity={selectedCity} />} />
        <Route path="/auth" element={<AuthForm />} />
        <Route path="/favourites" element={<Favorites />} />
        <Route path="/profile" element={<UserProfile />} />
        <Route path="/services" element={<ServicesPage />} />
        <Route path="/service/:id" element={<ServiceDetailPage />} />
        <Route path="/business-profile" element={<BusinessProfile/>} />
        <Route path="/booking-management" element={<BookingManagement />} />
        {/* Add more routes as needed */}

         {/* Protected Route */}
  <Route
    path="/register-service"
    element={
      <PrivateRoute>
        <RegisterService />
      </PrivateRoute>
    }
  />
      </Routes>
      <Footer />
       <ToastContainer
        position="top-center"
        autoClose={3000} // 3 seconds
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="colored"
      />
    </div>
  );
  
}
export default App;
