import axios from "axios";
// 臨時硬寫：先確保打得到後端
const base = "https://cda-backend-499q.onrender.com";
export default axios.create({ baseURL: base });
