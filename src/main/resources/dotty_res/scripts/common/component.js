class Component {
  constructor(props = {}) {
    this.props = props;
    this.prevProps = {};
    this.state = {};
  }

  setState(nextState) {
    const prevState = { ...this.state };
    if (typeof nextState === "function") {
      this.state = {
        ...this.state,
        ...nextState(this.state),
      };
    } else {
      this.state = {
        ...this.state,
        ...nextState,
      };
    }

    if (this.componentDidUpdate) {
      this.componentDidUpdate(prevState);
    }

    if (this.render) {
      this.render();
    }
  }
}
