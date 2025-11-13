const { ethers } = require("hardhat");

async function main() {
  const Vault = await ethers.getContractFactory("ConfidentialVault");
  const vault = await Vault.deploy();

  // 等待部署完成（Ethers v6 正確寫法）
  await vault.waitForDeployment();

  console.log("✅ ConfidentialVault deployed to:", await vault.getAddress());
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
