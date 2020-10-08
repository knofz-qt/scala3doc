class DocumentableList extends Component {
  constructor(props) {
    super(props);

    this.togglableRefs = [...findRefs(".tabbedcontent div[data-togglable]")];
    this.render(this.props);
  }

  filterLists = (inputValue, filters) => (togglableRef) => {
    return [...findRefs(".documentableList", togglableRef)].map((listRef) => {
      const visibleChildren = this.filterElements(listRef, inputValue, filters);
      ifVisible(visibleChildren.length, listRef, "block");
    });
  };

  filterElements = (listRef, inputValue, filters) => {
    return [...findRefs(".documentableElement", listRef)]
      .map((elementRef) => ({
        ref: elementRef,
        name: getText(findRef(".documentableName", elementRef)),
        description: getText(findRef(".documentableBrief", elementRef)),
      }))
      .filter(({ ref, ...data }) => {
        const isVisible = this.isAnyFilterSelected(ref, filters)
          ? this.includesInputValue(data, inputValue)
          : false;
        ifVisible(isVisible, ref, "table");
        toggleVisibility(isVisible, ref);
        return isVisible;
      });
  };

  isAnyFilterSelected = ({ dataset }, filters) => {
    return Object.keys(dataset)
      .filter((key) => startsWith(key, "f"))
      .some((key) => filters[key] && filters[key][dataset[key]]);
  };

  includesInputValue = ({ name, description }, inputValue) => {
    return name.includes(inputValue) || description.includes(inputValue);
  };

  render({ value, filters }) {
    this.togglableRefs.map(this.filterLists(value, filters));
  }
}
