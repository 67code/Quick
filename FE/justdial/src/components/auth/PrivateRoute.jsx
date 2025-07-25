import { useAuth } from "../../context/AuthContext";
import { Navigate } from "react-router-dom";

const PrivateRoute = ({ children }) => {
  const { user } = useAuth();

  // If not logged in, redirect to login
  if (!user) {
    return <Navigate to="/auth" replace />;
  }

  return children;
};

export default PrivateRoute;
