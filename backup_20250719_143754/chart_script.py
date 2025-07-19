import plotly.graph_objects as go
import plotly.express as px

# Create the architecture diagram using shapes and annotations
fig = go.Figure()

# Define layer positions and colors
layers = [
    {"name": "Presentation", "y": 2.5, "color": "#1FB8CD", "components": [
        "MainActivity", "MainScreen", "MainViewModel", "ClipSvc", "FloatSvc"
    ]},
    {"name": "Domain", "y": 1.5, "color": "#FFC185", "components": [
        "Use Cases", "Models", "Repo IF"
    ]},
    {"name": "Data", "y": 0.5, "color": "#ECEBD5", "components": [
        "Repo Impl", "Room DB", "SQLCipher", "EncryptMgr", "KeyStore"
    ]}
]

# Add layer backgrounds
for layer in layers:
    fig.add_shape(
        type="rect",
        x0=-0.5, y0=layer["y"]-0.4,
        x1=4.5, y1=layer["y"]+0.4,
        fillcolor=layer["color"],
        opacity=0.3,
        line=dict(width=0)
    )

# Add component boxes
for layer in layers:
    for i, comp in enumerate(layer["components"]):
        x_pos = i * 0.8
        fig.add_shape(
            type="rect",
            x0=x_pos-0.3, y0=layer["y"]-0.2,
            x1=x_pos+0.3, y1=layer["y"]+0.2,
            fillcolor=layer["color"],
            opacity=0.8,
            line=dict(width=2, color="white")
        )
        
        # Add component labels
        fig.add_annotation(
            x=x_pos, y=layer["y"],
            text=comp,
            showarrow=False,
            font=dict(size=10, color="white"),
            xanchor="center",
            yanchor="middle"
        )

# Add layer labels
for layer in layers:
    fig.add_annotation(
        x=-0.3, y=layer["y"],
        text=layer["name"],
        showarrow=False,
        font=dict(size=12, color="black"),
        xanchor="right",
        yanchor="middle"
    )

# Add arrows showing data flow
# From Presentation to Domain
fig.add_annotation(
    x=1.5, y=2.1,
    ax=1.5, ay=1.9,
    arrowhead=2,
    arrowsize=1,
    arrowwidth=2,
    arrowcolor="gray"
)

# From Domain to Data
fig.add_annotation(
    x=1.5, y=1.1,
    ax=1.5, ay=0.9,
    arrowhead=2,
    arrowsize=1,
    arrowwidth=2,
    arrowcolor="gray"
)

# Update layout
fig.update_layout(
    title="Android App Architecture",
    xaxis=dict(
        showgrid=False,
        showticklabels=False,
        range=[-0.6, 4.2]
    ),
    yaxis=dict(
        showgrid=False,
        showticklabels=False,
        range=[0, 3]
    ),
    showlegend=False,
    plot_bgcolor="white"
)

# Save the chart
fig.write_image("android_architecture_diagram.png")