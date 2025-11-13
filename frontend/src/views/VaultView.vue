<template>
  <div class="container">
    <h2>Zama + Stable Confidential Vault</h2>

    <!-- 原本區塊：以現成密文上鏈與讀回 -->
    <div class="card">
      <label>Encrypted Input (Hex)</label>
      <textarea v-model="cipherHex" placeholder="e.g. 0x1234abcd"></textarea>
      <button @click="submitMetric" :disabled="loading">
        {{ loading ? 'Submitting...' : 'Submit Metric' }}
      </button>
      <div v-if="txHash"><p>✅ Tx Hash</p><code>{{ txHash }}</code></div>
      <hr />
      <button @click="getResult">Get Encrypted Result</button>
      <div v-if="result"><p>🔒 Encrypted Result</p><code>{{ result }}</code></div>
    </div>

    <!-- 新增區塊：FHE Mock E2E（明文→加密→上鏈→評估→回寫→取回→解密） -->
    <div class="card" style="margin-top:20px">
      <h3>FHE Mock End-to-End</h3>

      <label>Plain Input</label>
      <input v-model="plain" class="w-full" placeholder="e.g. hello-zama-stable" />

      <div class="mt-2">
        <button @click="encryptPlain">1) Encrypt → Cipher</button>
      </div>
      <div v-if="cipherHexFromPlain">
        <p>🔐 Cipher (from plain)</p>
        <code>{{ cipherHexFromPlain }}</code>
      </div>

      <div class="mt-2">
        <button @click="submitMetricFromPlain" :disabled="!cipherHexFromPlain || loading">
          2) Submit Metric (on-chain)
        </button>
        <div v-if="txSubmitFromPlain"><p>✅ Tx Hash</p><code>{{ txSubmitFromPlain }}</code></div>
      </div>

      <div class="mt-2">
        <button @click="evalAndPost">3) Eval & Post Result (backend → on-chain)</button>
        <div v-if="txEval"><p>✅ Tx Hash</p><code>{{ txEval }}</code></div>
      </div>

      <div class="mt-2">
        <button @click="getAndDecrypt">4) Get & Decrypt Result</button>
        <p>🔒 cipher: <code>{{ resultCipher }}</code></p>
        <p>🟢 plain: <code>{{ resultPlain }}</code></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import { ref } from "vue";

/** 你原本的狀態（保留） */
const cipherHex = ref("0x1234abcd");
const txHash = ref("");
const result = ref("");
const loading = ref(false);

async function submitMetric(){
  if(!cipherHex.value) return alert("Please input cipherHex");
  loading.value = true;
  try{
    const r = await axios.post('/vault/submit', null, { params:{ cipherHex: cipherHex.value }});
    txHash.value = r.data.txHash || "";
  } finally { loading.value = false; }
}
async function getResult(){
  const r2 = await axios.get('/vault/result');
  result.value = r2.data.encryptedResult || "";
}

/** 第 5 步：FHE mock 端到端 */
const plain = ref("hello-zama-stable");
const cipherHexFromPlain = ref("");
const txSubmitFromPlain = ref("");
const txEval = ref("");
const resultCipher = ref("");
const resultPlain = ref("");

async function encryptPlain(){
  const r = await axios.post('/fhe/encrypt', null, { params:{ plain: plain.value }});
  cipherHexFromPlain.value = r.data.cipherHex || "";
}

async function submitMetricFromPlain(){
  if(!cipherHexFromPlain.value) return alert("請先 Encrypt");
  loading.value = true;
  try{
    const r = await axios.post('/vault/submit', null, { params:{ cipherHex: cipherHexFromPlain.value }});
    txSubmitFromPlain.value = r.data.txHash || "";
  } finally { loading.value = false; }
}

async function evalAndPost(){
  const r = await axios.post('/fhe/eval-and-post');
  txEval.value = r.data.txHash || "";
}

async function getAndDecrypt(){
  const r = await axios.get('/fhe/decrypt-result');
  resultCipher.value = r.data.cipherHex || "0x";
  resultPlain.value  = r.data.plain || "";
}
</script>

<style>
.container{max-width:900px;margin:40px auto;padding:20px}
.card{background:#fffbea;padding:20px;border-radius:16px;box-shadow:0 0 10px #ccc}
textarea{width:100%;height:100px;margin-bottom:12px}
input.w-full{width:100%;padding:8px;margin-bottom:12px}
.mt-2{margin-top:12px}
button{background:#ffcf33;border:0;padding:10px 20px;border-radius:8px;cursor:pointer}
code{display:block;background:#f3f3f3;padding:6px;border-radius:6px;word-break:break-all}
</style>
