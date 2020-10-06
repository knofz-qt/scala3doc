class FilterGroup extends Component {
  constructor(props) {
    super(props);

    this.state = {
      isVisible: false,
    };

    this.filterToggleRef = findRef("filterToggleButton");
    this.onClickFn = withEvent(
      this.filterToggleRef,
      "click",
      this.props.onFilterToggleClick
    );
  }
}
