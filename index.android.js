'use strict';

import {
  NativeModules
} from 'react-native';
import { mapParameters } from './utils';
const Braintree = NativeModules.Braintree;

module.exports = {
  setupWithToken(token) {
    return new Promise(function (resolve, reject) {
      Braintree.setupWithToken(token, test => resolve(test), err => reject(err));
    });
  },

  getCardNonce(parameters = {}) {
    return new Promise(function (resolve, reject) {
      Braintree.getCardNonce(
        mapParameters(parameters),
        nonce => resolve(nonce),
        err => reject(err)
      );
    });
  },
  check3DSecure(parameters = {}) {
    return new Promise(function (resolve, reject) {
      console.log(parameters)
      Braintree.check3DSecure(parameters, nonce => resolve(nonce),
        err => reject(err));
    });
  },
  showPayPalViewController(amount, shippingrequired) {
    return new Promise(function (resolve, reject) {
      Braintree.paypalRequest(amount, (nonce
        // ,
        // email,
        // firstName,
        // lastName,
        // billingAddress,
        // shippingAddress
      ) => resolve({
        nonce,
        // email,
        // firstName,
        // lastName,
        // billingAddress,
        // shippingAddress
      }), error => reject(error));
    });
  },
};