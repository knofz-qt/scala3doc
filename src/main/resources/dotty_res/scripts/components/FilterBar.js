class FilterBar extends Component {
  constructor(props) {
    super(props);

    this.inputComp = new Input();
  }

  componentDidMount() {}

  componentWillUnmount() {
    this.inputComp.componentWillUnmount();
  }

  render() {
    this.inputComp.render();
  }
}

init(() => new FilterBar());
