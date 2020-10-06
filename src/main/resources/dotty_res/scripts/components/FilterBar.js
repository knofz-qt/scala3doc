class FilterBar extends Component {
  constructor(props) {
    super(props);

    this.inputComp = new Input({ onInputChange: this.onInputChange });
    this.filterGroupComp = new FilterGroup();
  }

  componentDidUpdate() {
    console.log(this.state);
  }

  componentWillUnmount() {
    this.inputComp.componentWillUnmount();
  }

  onInputChange = (value) => {
    console.log(value);
  };

  render() {}
}

init(() => new FilterBar());
