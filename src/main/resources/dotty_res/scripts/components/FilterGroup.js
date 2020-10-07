const defaultFilterGroup = {
  FOrdering: ["Alphabetical"],
};

class FilterGroup extends Component {
  constructor(props) {
    super(props);

    this.state = {
      groups: this.generateGroups(),
      isVisible: false,
    };

    this.filterToggleRef = findRef("filterToggleButton");
    this.filterLowerContainerRef = findRef("filterLowerContainer");

    this.onClickFn = withEvent(
      this.filterToggleRef,
      "click",
      this.props.onFilterToggleClick
    );

    this.render();
  }

  generateGroups() {
    return {
      ...defaultFilterGroup,
      ...[...findRefs("documentableElement")].reduce(
        this.getGroupFromDataset,
        {}
      ),
    };
  }

  getGroupFromDataset(group, { dataset }) {
    Object.entries(dataset).map(([key, value]) => {
      if (!group[key]) {
        group[key] = [value];
      } else if (!group[key].includes(value)) {
        group[key].push(value);
      }
    });
    return group;
  }

  getFilterGroup(title, values) {
    return `
      <div class="filterGroup">
        <span class="groupTitle">${title.substring(1)}</span>
        <div class="filterList">
          ${values.map(
            (value) => `<button class="filterButtonItem">${value}</button>`
          )}
        </div>
      </div>
    `;
  }

  render() {
    const { groups } = this.state;

    this.filterLowerContainerRef.innerHTML = Object.entries(
      groups
    ).map(([key, values]) => this.getFilterGroup(key, values));
  }
}
