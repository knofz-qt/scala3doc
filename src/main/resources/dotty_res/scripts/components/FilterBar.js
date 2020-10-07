class FilterBar extends Component {
  constructor(props) {
    super(props);

    this.state = {
      value: "",
      isVisible: false,
    };

    this.filterBarRef = findRef(".documentableFilter");

    this.inputComp = new Input({ onInputChange: this.onInputChange });
    this.documentableList = new DocumentableList({ value: this.state.value });
    this.filterGroupComp = new FilterGroup({
      onFilterToggleClick: this.onFilterToggleClick,
    });
  }

  onInputChange = (value) => {
    this.setState({ value }, () => {
      this.documentableList.render({ value: this.state.value });
      this.filterGroupComp.render();
    });
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
