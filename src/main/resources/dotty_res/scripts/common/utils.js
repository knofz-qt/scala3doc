const findRef = (className, element = document) =>
  element.querySelector(`.${className}`);

const findRefs = (className, element = document) =>
  element.querySelectorAll(`.${className}`);

const withEvent = (element, listener, callback) => {
  element.addEventListener(listener, callback);
  return () => element.removeEventListener(listener, callback);
};

const init = (cb) => window.addEventListener("DOMContentLoaded", cb);
