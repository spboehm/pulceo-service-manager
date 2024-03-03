# pulceo-service-manager

## General Prerequisites

- Make sure that the following ports are available on the local system:
    - `80/tcp`
    - `443/tcp`
    - `40476/tcp` (for k3d API server)

## Create a free MQTT broker (recommended)

- Create a basic MQTT broker on [HiveMQ](https://console.hivemq.cloud/?utm_source=HiveMQ+Pricing+Page&utm_medium=serverless+signup+CTA+Button&utm_campaign=HiveMQ+Cloud+PaaS&utm_content=serverless)
- Make sure that you select the free plan: Serverless (Free)

## Create your own MQTT broker (optional)

**TODO: Add a guide on how to create a local MQTT broker**
```bash
k3d cluster create pulceo-test --api-port 40476 --port 80:80@loadbalancer
```

## Run with k3d

**[TODO]: Add a step to generate the secrets**
- Apply the following kubernetes manifest to the cluster
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create configmap psm-configmap \
  --from-literal=PRM_HOST=pulceo-resource-manager
```
```bash
kubectl --kubeconfig=/home/$USER/.kube/config create secret generic psm-credentials \
  --from-literal=PNA_MQTT_BROKER_URL=${PNA_MQTT_BROKER_URL} \
  --from-literal=PNA_MQTT_CLIENT_USERNAME=${PNA_MQTT_CLIENT_USERNAME} \
  --from-literal=PNA_MQTT_CLIENT_PASSWORD=${PNA_MQTT_CLIENT_PASSWORD}
```
```bash
kubectl apply -f psm-deployment.yaml
```