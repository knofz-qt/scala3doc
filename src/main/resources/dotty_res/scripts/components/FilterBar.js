class FilterBar extends Component {
  constructor(props) {
    super(props);

    this.state = {
      isVisible: false,
    };

    this.filterBarRef = findRef("documentableFilter");

    this.inputComp = new Input({ onInputChange: this.onInputChange });
    this.filterGroupComp = new FilterGroup({
      onFilterToggleClick: this.onFilterToggleClick,
    });
  }

  componentWillUnmount() {
    this.inputComp.componentWillUnmount();
  }

  onInputChange = (value) => {
    console.log(value);
  };

  onFilterToggleClick = () => {
    this.setState((prevState) => ({ isVisible: !prevState.isVisible }));
  };

  render() {
    const { isVisible } = this.state;

    if (this.filterBarRef) {
      if (isVisible) {
        this.filterBarRef.classList.add("active");
      } else {
        this.filterBarRef.classList.remove("active");
      }
    }
  }
}

init(() => new FilterBar());
