var TemplateFile = React.createClass({
    render: function () {
        return (
            <div class="templateFile">
                <h4>{this.props.name}</h4>
                {this.props.sizeInBytes}
            </div>
        );
    }
});

var TemplateFileList = React.createClass({
    getInitialState: function () {
        return {data: []};
    },
    componentDidMount: function () {
        $.ajax({
            url: this.props.url,
            dataType: 'json',
            cache: false,
            success: function (data) {
                this.setState({data: data});
            }.bind(this),
            error: function (xhr, status, err) {
                console.error(this.props.url, status, err.toString());
            }.bind(this)
        });
    },
    render: function () {
        var templates = this.state.data.map(function (template) {
            return <TemplateFile name={template.name} sizeInBytes={template.sizeInBytes}/>;
        });
        return (
            <div class="templateFileList">
                {templates}
            </div>
        );
    }
});

ReactDOM.render(
    <TemplateFileList url="/template-views"/>,
    document.getElementById("templateFileList")
);
