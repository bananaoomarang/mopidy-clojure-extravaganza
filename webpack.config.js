const path = require('path');
const webpack = require('webpack');

const prod = process.env.NODE_ENV === "production";

module.exports = {
  entry: './lib/modules.js',
  output: {
    path: "./resources/public/js",
    filename: prod ? 'bundle.min.js' : 'bundle.js'
  },
  module: {
    loaders: [
      { test: /.jsx?$/, loader: 'babel-loader', include: "./lib" }
    ]
  },
  plugins: [
    new webpack.DefinePlugin(
      {'process.env': { 'NODE_ENV': prod ? '"production"' : '"development"'}})
  ]
}
