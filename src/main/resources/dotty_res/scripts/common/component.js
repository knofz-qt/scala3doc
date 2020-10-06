class Component {
  state = {};

  constructor(props = {}) {
    this.props = props;
    this.mounted = false;

    this.renderComp(this.state);
  }

  mountComp() {
    if (typeof this.componentDidMount === "function") {
      this.componentDidMount();
    }
    this.renderComp(this.state);
  }

  updateComp() {
    if (typeof this.componentDidUpdate === "function") {
      this.componentDidUpdate();
    }
    this.renderComp(this.state);
  }

  unmount() {
    if (typeof this.componentWillUnmount === "function") {
      this.componentWillUnmount();
    }
  }

  setState(nextState) {
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

    this.renderComp(prevState);
  }

  renderComp(prevState) {
    if (!this.mounted) {
      this.mounted = true;
      this.mountComp();
    }

    if (this.mounted && this.state !== prevState) {
      this.updateComp();
    }
    this.render();
  }
}
