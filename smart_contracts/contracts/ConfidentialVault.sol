// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title ConfidentialVault - store user-submitted ciphertext for FHE analytics
contract ConfidentialVault {
    struct EncryptedMetric {
        bytes ciphertext;   // e.g. FHE-encrypted balance delta / vector
        uint256 updatedAt;
    }

    mapping(address => EncryptedMetric) private metrics;

    event MetricSubmitted(address indexed user, uint256 ts, uint256 size);
    event ResultPosted(address indexed user, uint256 ts, uint256 size);

    // user submits their own encrypted metric (client-side encrypted)
    function submitMetric(bytes calldata _cipher) external {
        metrics[msg.sender] = EncryptedMetric({
            ciphertext: _cipher,
            updatedAt: block.timestamp
        });
        emit MetricSubmitted(msg.sender, block.timestamp, _cipher.length);
    }

    // user retrieves their last submitted ciphertext (for verification / re-run)
    function getMyMetric() external view returns (bytes memory, uint256) {
        EncryptedMetric memory m = metrics[msg.sender];
        return (m.ciphertext, m.updatedAt);
    }

    // (optional) backend posts encrypted result for user (e.g., encrypted risk score)
    mapping(address => bytes) private encryptedResults;

    function postEncryptedResult(address user, bytes calldata resultCipher) external {
        // In production: restrict to a relayer/oracle address with access control
        encryptedResults[user] = resultCipher;
        emit ResultPosted(user, block.timestamp, resultCipher.length);
    }

    function getMyEncryptedResult() external view returns (bytes memory) {
        return encryptedResults[msg.sender];
    }
}
