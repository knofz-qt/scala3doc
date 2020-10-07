const defaultFilterGroup = {
  FOrdering: ["Alphabetical"],
};

class FilterGroup extends Component {
  constructor(props) {
    super(props);

    this.state = {
      isVisible: false,
    };

    this.filterToggleRef = findRef(".filterToggleButton");
    this.filterLowerContainerRef = findRef(".filterLowerContainer");

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
      ...[...findRefs(`.documentableElement[data-visibility="true"]`)].reduce(
        this.getGroupFromDataset,
        {}
      ),
    };
  }

  getGroupFromDataset(group, { dataset }) {
    Object.entries(dataset).map(([key, value]) => {
      if (!startsWith(key, "f")) {
        return;
      }
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
    const groups = this.generateGroups();

    attachDOM(
      this.filterLowerContainerRef,
      Object.entries(groups).map(([key, values]) =>
        this.getFilterGroup(key, values)
      )
    );
  }
}
