require("dotenv").config();
require("@nomicfoundation/hardhat-ethers");

const STABLE_RPC_URL = process.env.STABLE_RPC_URL || "https://rpc.testnet.stable.xyz";
const PRIVATE_KEY = process.env.PRIVATE_KEY || "0x0000000000000000000000000000000000000000";

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.20",
  networks: {
    stable: {
      url: STABLE_RPC_URL,
      accounts: [PRIVATE_KEY],
      chainId: 2201
    }
  }
};
