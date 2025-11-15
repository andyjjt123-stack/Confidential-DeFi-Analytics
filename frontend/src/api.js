import axios from "axios";
// 臨時硬寫：先確保打得到後端
const base = "https://cda-backend-499q.onrender.com";
export default axios.create({ baseURL: base });

//import axios from "axios";
// 本地開發（Vite 代理）可讓它為空字串；上線則由打包時的變數帶入
//const base = import.meta.env.VITE_API_BASE || "";
//export default axios.create({ baseURL: base });//

