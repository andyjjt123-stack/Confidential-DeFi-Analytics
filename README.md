# 🌐 Zama + Stable Confidential DeFi Analytics  
**End-to-End Full Homomorphic Encryption (FHE) Demo**

> 🇨🇳 中文版本見下方  
> 🇺🇸 English version first  

---

## 🇺🇸 English Overview

### 🧩 Project Architecture
```
Confidential-DeFi-Analytics/
├── smart_contracts/   # Solidity + Hardhat (ConfidentialVault)
├── backend/           # Spring Boot + Web3j + pluggable FHE engine
└── frontend/          # Vue 3 + Vite (Zama-branded UI)
```

### 💡 Concept
This project demonstrates a **confidential DeFi analytics flow**:
> Frontend input (plaintext) →  
> Mock FHE encryption →  
> On-chain submit →  
> Backend evaluation →  
> On-chain result →  
> Decrypt to plaintext.

The FHE module is fully pluggable, so you can later integrate a real Zama FHE or TFHE backend.

---

### ⚙️ Tech Stack
| Layer | Technology |
|-------|-------------|
| Smart Contract | Solidity (Hardhat 2.26.x) |
| Backend | Spring Boot 3.5.7, Web3j, Maven Wrapper |
| Frontend | Vue 3 + Vite + Axios |
| FHE Engine | Mock Java FHE (reversible string transform) |
| Blockchain | Stable Testnet |

---

### 🚀 Quick Start

#### 1️⃣ Smart Contracts
```bash
cd smart_contracts
cp .env.example .env
# Fill in:
# PRIVATE_KEY=0x...
# STABLE_RPC_URL=https://rpc.testnet.stable.xyz
npx hardhat compile
npx hardhat run scripts/deploy.js --network stable
```

Copy deployed address to backend `.env`:
```
CONTRACT_ADDRESS=0xYourVaultAddress
CHAIN_ID=2201
STABLE_RPC_URL=https://rpc.testnet.stable.xyz
PRIVATE_KEY=0xYourPrivateKey
```

---

#### 2️⃣ Backend (Spring Boot)
```bash
cd backend
.\mvnw.cmd spring-boot:run  # Windows
# or ./mvnw spring-boot:run  # Linux/macOS
```
Runs on: **http://localhost:8080**

---

#### 3️⃣ Frontend (Vue 3)
```bash
cd frontend
npm install
npm run dev
```
Runs on: **http://localhost:5173**

Proxy routes (in `vite.config.js`):
```js
server: {
  proxy: {
    '/vault': { target: 'http://localhost:8080', changeOrigin: true },
    '/fhe': { target: 'http://localhost:8080', changeOrigin: true },
  }
}
```

---

#### 4️⃣ Test the Flow
Input a plain string (e.g. `hello-zama-stable`)  
and click:
1. 🧠 Encrypt → Cipher  
2. 🪙 Submit Metric (on-chain)  
3. ⚙️ Eval & Post Result  
4. 🔓 Get & Decrypt Result  

You’ll see reversed text (mock FHE decryption):  
> `elbats-amaz-olleh`

---

### 🧱 FHE Engine Architecture
Located in:
```
backend/src/main/java/io/github/andyjjt123/cda/fhe/
```

| File | Description |
|------|--------------|
| `FheEngine.java` | Interface |
| `MockFheEngine.java` | Demo reversible implementation |
| `FheConfig.java` | Spring Bean provider |

Replace `MockFheEngine` with your own FHE SDK (Zama, Concrete, TFHE, etc.) for production use.

---

### 🪄 Scripts
**run-all.ps1 (Windows)**
```powershell
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm run dev"
```

**run-all.sh (macOS/Linux)**
```bash
#!/usr/bin/env bash
(cd backend && ./mvnw spring-boot:run) & 
(cd frontend && npm run dev) &
wait
```

---

## 🇨🇳 中文版本說明

### 🧩 專案架構
此專案展示「**隱私運算＋去中心化分析（Confidential DeFi Analytics）**」的完整流程：

- 前端輸入明文
- FHE 模擬加密（Mock Engine）
- 上鏈儲存密文
- 後端運算再上鏈
- 最後解密取回可讀結果

FHE 模組為可插拔式介面，可日後直接整合 Zama FHE 或 TFHE SDK。

---

### ⚙️ 技術組成
| 層級 | 技術 |
|------|------|
| 智能合約 | Solidity (Hardhat) |
| 後端 | Spring Boot + Web3j |
| 前端 | Vue 3 + Vite |
| 加密引擎 | Mock FHE（字串反轉） |
| 區塊鏈 | Stable 公測鏈 |

---

### 🚀 執行步驟
1️⃣ **部署合約**
```bash
cd smart_contracts
npx hardhat compile
npx hardhat run scripts/deploy.js --network stable
```

2️⃣ **啟動後端**
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

3️⃣ **啟動前端**
```bash
cd frontend
npm install
npm run dev
```

4️⃣ **測試流程**
輸入明文 → 點擊加密 → 上鏈 → 取回 → 解密  
最後會看到反轉的字串結果，如：
```
elbats-amaz-olleh
```

---

### 💡 可替換模組
若要使用真實 FHE，可替換：
```
FheEngine.java / MockFheEngine.java
```

---

### 🧾 授權
MIT License © 2025 andyjjt123  
Built with ❤️ using Zama FHE + Stable Network
