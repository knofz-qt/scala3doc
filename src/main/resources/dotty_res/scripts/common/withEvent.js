const withEvent = (element, listener, callback) => {
  element.addEventListener(listener, callback);
  return () => element.removeEventListener(listener, callback);
};
