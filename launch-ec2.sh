#!/bin/bash

set -e

echo "Updating system packages..."
sudo apt update -y

echo "Installing required packages..."
sudo apt install -y nginx tar git curl docker.io

echo "Installing Node.js and npm..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

echo "Starting and enabling Docker..."
sudo systemctl enable docker
sudo systemctl start docker

echo "Adding ubuntu user to Docker group..."
sudo usermod -aG docker ubuntu

echo "Starting and enabling Nginx..."
sudo systemctl enable nginx
sudo systemctl start nginx

echo "Installed versions:"
echo "--------------------------------"

echo "Nginx Version:"
nginx -v

echo "Git Version:"
git --version

echo "Node.js Version:"
node -v

echo "npm Version:"
npm -v

echo "Docker Version:"
docker --version

echo "--------------------------------"
echo "Installation completed successfully."
echo "Logout/Login again or run 'newgrp docker' manually to use Docker without sudo."