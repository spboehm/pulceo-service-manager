import http from "k6/http";
import {randomString} from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import {check, sleep} from "k6";

// Test configuration
export const options = {
    thresholds: {
        // Assert that 99% of requests finish within 3000ms.
        http_req_duration: ["p(99) < 3000"],
    },
    // Ramp the number of virtual users up and down
    stages: [
        {duration: "30s", target: 200},
        {duration: "30s", target: 400},
        {duration: "30s", target: 600},
        {duration: "30s", target: 800},
        {duration: "30s", target: 1000},
    ],
};

// Simulated user behavior
export default function () {
    let res = http.post("http://localhost:7979/api/v1/tasks", JSON.stringify({
        sizeOfWorkload: 10000,
        payload: randomString(512)
    }), {headers: {"Content-Type": "application/json"}});
    // Validate response status
    check(res, {"status was 201": (r) => r.status === 201});
    sleep(1);
}
