class Input extends Component {
  constructor(props) {
    super(props);

    this.inputRef = findRef("filterableInput");
  }

  onInputChange = ({ currentTarget: { value } }) => {
    console.log(value);
  };

  componentDidMount() {
    this.onChangeFn = withEvent(this.inputRef, "onChange", this.onInputChange);
  }

  componentWillUnmount() {
    if (this.onChangeFn) {
      this.onChangeFn();
    }
  }
}
